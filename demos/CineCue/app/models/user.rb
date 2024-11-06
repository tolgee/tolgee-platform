require 'scrypt'

class User < ApplicationRecord

  validates :email, format: {
    with: /\A[^@\s]+@[^@\s]+\z/,
    message: "must be a valid email format"
  }

  validates :username, format: {
    with: /\A[a-z0-9]+\z/i,
    message: "can only contain letters and numbers"
  }
  validates :username, length: { minimum: 5, maximum: 24 }
  validates :username, uniqueness: true

  validates :lang, inclusion: { in: %w(en ar tr ur hi es fr), message: "%{value} is not a valid option." }

  validate :password_validation

  def password=(new_password)
    @password = new_password
    self.password_digest = SCrypt::Password.create(new_password)
  end

  def password_validation
    if @password.present?
      unless @password =~ /\A(?=.{8,32})[a-z0-9!#@]*\z/i
        errors.add(:password, "must be between 8 and 64 characters and include valid characters")
      end
    end
  end

  def check?(password)
    SCrypt::Password.new(self.password_digest) == password
  end

end
